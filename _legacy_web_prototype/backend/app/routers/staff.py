from datetime import datetime, timezone, timedelta
from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from sqlmodel import Session, select
from app.database import get_session
from app.models import (
    Staff, StaffRole, Bus, BusStatus, BusType, Route, Trip,
    TripStatus, SeatEstimate, MovementLog, MovementType, Announcement
)
from app.auth import hash_pin, verify_pin, generate_session_token

router = APIRouter(prefix="/api/v1/staff", tags=["Terminal Staff Operations"])

# Pydantic Request Schemas
class StaffRegisterRequest(BaseModel):
    full_name: str
    role: StaffRole = StaffRole.INSPECTOR
    contact_info: str
    pin: str = Field(min_length=4, max_length=6, description="4 to 6 digit security PIN")

class StaffLoginRequest(BaseModel):
    full_name: str
    pin: str

class RecordMovementRequest(BaseModel):
    staff_id: int
    bus_id: int
    movement_type: MovementType
    route_id: int
    driver_name: str
    driver_license: str
    seat_status: SeatEstimate = SeatEstimate.AVAILABLE
    notes: Optional[str] = None

class MovementCorrectionRequest(BaseModel):
    staff_id: int
    driver_name: Optional[str] = None
    driver_license: Optional[str] = None
    seat_status: Optional[SeatEstimate] = None
    notes: Optional[str] = None

class PostAnnouncementRequest(BaseModel):
    staff_id: int
    title: str
    content: str
    priority: str = "NORMAL"

@router.post("/register")
def register_staff(req: StaffRegisterRequest, session: Session = Depends(get_session)):
    """Registers a terminal staff member (Inspector, Dispatcher, Conductor) with a personal PIN."""
    existing = session.exec(select(Staff).where(Staff.full_name == req.full_name)).first()
    if existing:
        raise HTTPException(status_code=400, detail="Staff member with this name is already registered")

    if not req.pin.isdigit():
        raise HTTPException(status_code=400, detail="PIN must contain numbers only")

    pin_hash, pin_salt = hash_pin(req.pin)
    staff = Staff(
        full_name=req.full_name.strip(),
        role=req.role,
        contact_info=req.contact_info.strip(),
        pin_hash=pin_hash,
        pin_salt=pin_salt
    )
    session.add(staff)
    session.commit()
    session.refresh(staff)

    token = generate_session_token(staff.id, staff.full_name)
    return {
        "message": f"Successfully registered {staff.full_name}",
        "staff": {
            "id": staff.id,
            "full_name": staff.full_name,
            "role": staff.role.value,
            "contact_info": staff.contact_info
        },
        "session_token": token
    }

@router.post("/login")
def login_staff(req: StaffLoginRequest, session: Session = Depends(get_session)):
    """Swift terminal login using Name + 4-6 digit PIN."""
    staff = session.exec(select(Staff).where(Staff.full_name == req.full_name.strip())).first()
    if not staff:
        raise HTTPException(status_code=404, detail="Staff member not found. Please register first.")

    if not verify_pin(req.pin, staff.pin_hash, staff.pin_salt):
        raise HTTPException(status_code=401, detail="Incorrect PIN. Please re-enter.")

    token = generate_session_token(staff.id, staff.full_name)
    return {
        "message": "Authentication successful",
        "staff": {
            "id": staff.id,
            "full_name": staff.full_name,
            "role": staff.role.value,
            "contact_info": staff.contact_info
        },
        "session_token": token
    }

@router.get("/roster")
def get_staff_roster(session: Session = Depends(get_session)):
    """Returns registered staff names for quick touch selection during shift start."""
    staff_list = session.exec(select(Staff).where(Staff.is_active == True)).all()
    return {
        "staff": [
            {"id": s.id, "full_name": s.full_name, "role": s.role.value}
            for s in staff_list
        ]
    }

@router.get("/buses")
def list_buses_for_staff(session: Session = Depends(get_session)):
    """List all fleet buses for rapid inspector logging selection."""
    buses = session.exec(select(Bus).order_by(Bus.bus_number)).all()
    return {
        "buses": [
            {
                "id": b.id,
                "bus_number": b.bus_number,
                "plate_number": b.plate_number,
                "bus_type": b.bus_type.value,
                "current_status": b.current_status.value,
                "current_location": b.current_location
            }
            for b in buses
        ]
    }

@router.post("/movements")
def record_bus_movement(req: RecordMovementRequest, session: Session = Depends(get_session)):
    """
    Records an arrival or departure movement.
    Addresses HCI Heuristic #4 (Error Prevention) & User Need #3.
    Requires staff review before submission.
    """
    staff = session.get(Staff, req.staff_id)
    if not staff:
        raise HTTPException(status_code=404, detail="Staff record not found")

    bus = session.get(Bus, req.bus_id)
    if not bus:
        raise HTTPException(status_code=404, detail="Bus not found")

    route = session.get(Route, req.route_id)
    if not route:
        raise HTTPException(status_code=404, detail="Route not found")

    now = datetime.now(timezone.utc)

    # Update Bus Status
    if req.movement_type == MovementType.ARRIVAL:
        bus.current_status = BusStatus.AT_TERMINAL
        bus.current_location = route.destination
    else: # DEPARTURE
        bus.current_status = BusStatus.EN_ROUTE
        bus.current_location = f"En route to {route.destination}"

    bus.last_updated_at = now
    session.add(bus)

    # Create or update associated Trip
    trip_status = TripStatus.ARRIVED if req.movement_type == MovementType.ARRIVAL else TripStatus.DEPARTED
    trip = Trip(
        bus_id=bus.id,
        route_id=route.id,
        driver_name=req.driver_name.strip(),
        driver_license=req.driver_license.strip().upper(),
        service_regime=bus.bus_type,
        status=trip_status,
        seat_status=req.seat_status,
        actual_departure=now if req.movement_type == MovementType.DEPARTURE else None,
        actual_arrival=now if req.movement_type == MovementType.ARRIVAL else None,
        last_updated_at=now,
        notes=req.notes
    )
    session.add(trip)
    session.commit()
    session.refresh(trip)

    # Record Immutable Movement Log
    log = MovementLog(
        trip_id=trip.id,
        bus_id=bus.id,
        staff_id=staff.id,
        movement_type=req.movement_type,
        recorded_at=now,
        notes=req.notes or f"Logged by {staff.role.value.capitalize()} {staff.full_name}"
    )
    session.add(log)
    session.commit()
    session.refresh(log)

    return {
        "success": True,
        "message": f"Successfully recorded {req.movement_type.value} for {bus.bus_number}",
        "log_id": log.id,
        "trip_id": trip.id,
        "recorded_at": log.recorded_at.isoformat(),
        "bus_status": bus.current_status.value
    }

@router.patch("/movements/{log_id}/correct")
def correct_bus_movement(log_id: int, req: MovementCorrectionRequest, session: Session = Depends(get_session)):
    """
    HCI Heuristic #5 (User Control & Freedom).
    Allows staff to correct accidental typos within 30 minutes of logging without breaking auditability.
    """
    log = session.get(MovementLog, log_id)
    if not log:
        raise HTTPException(status_code=404, detail="Movement log not found")

    # Check 30-minute grace window
    age = (datetime.now(timezone.utc) - log.recorded_at).total_seconds() / 60
    if age > 30:
        raise HTTPException(status_code=400, detail="Correction window (30 mins) has expired for this record")

    if log.trip_id:
        trip = session.get(Trip, log.trip_id)
        if trip:
            if req.driver_name:
                trip.driver_name = req.driver_name.strip()
            if req.driver_license:
                trip.driver_license = req.driver_license.strip().upper()
            if req.seat_status:
                trip.seat_status = req.seat_status
            if req.notes:
                trip.notes = req.notes
            trip.last_updated_at = datetime.now(timezone.utc)
            session.add(trip)

    # Mark log as corrected
    log.is_correction = True
    if req.notes:
        log.notes = f"{log.notes or ''} [CORRECTED: {req.notes}]"
    session.add(log)
    session.commit()

    return {"success": True, "message": "Record corrected successfully"}

@router.post("/announcements")
def post_announcement(req: PostAnnouncementRequest, session: Session = Depends(get_session)):
    """HCI Heuristic #1: Broadcasts megaphone updates digitally to all commuters."""
    staff = session.get(Staff, req.staff_id)
    if not staff:
        raise HTTPException(status_code=404, detail="Staff record not found")

    announcement = Announcement(
        title=req.title.strip(),
        content=req.content.strip(),
        priority=req.priority,
        created_at=datetime.now(timezone.utc),
        created_by_name=f"{staff.role.value.capitalize()} {staff.full_name}",
        is_active=True
    )
    session.add(announcement)
    session.commit()
    session.refresh(announcement)

    return {
        "success": True,
        "message": "Announcement broadcast successfully",
        "announcement_id": announcement.id
    }
