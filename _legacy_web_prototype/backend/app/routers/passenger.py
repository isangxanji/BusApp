from datetime import datetime, timezone
from typing import Optional, List
from fastapi import APIRouter, Depends, Query
from sqlmodel import Session, select, func
from app.database import get_session
from app.models import (
    Bus, BusType, BusStatus, Route, Trip, TripStatus, MovementLog,
    MovementType, Announcement, Staff
)

router = APIRouter(prefix="/api/v1", tags=["Passenger Public API"])

@router.get("/dashboard")
def get_dashboard_summary(session: Session = Depends(get_session)):
    """Summary dashboard for the passenger mobile home screen (Wireframe 3)."""
    now = datetime.now(timezone.utc)

    # Query counts of buses at terminal
    buses_at_terminal = session.exec(
        select(Bus).where(Bus.current_status == BusStatus.AT_TERMINAL)
    ).all()

    total_available = len(buses_at_terminal)
    regular_count = sum(1 for b in buses_at_terminal if b.bus_type == BusType.REGULAR)
    twenty_four_count = sum(1 for b in buses_at_terminal if b.bus_type == BusType.TWENTY_FOUR_HOUR)

    # Get the latest recorded movement or update across system
    latest_log = session.exec(
        select(MovementLog).order_by(MovementLog.recorded_at.desc())
    ).first()

    last_sync_time = latest_log.recorded_at if latest_log else now
    diff_minutes = max(0, int((now - last_sync_time).total_seconds() / 60))

    # Active announcements (digital megaphone)
    announcements = session.exec(
        select(Announcement).where(Announcement.is_active == True).order_by(Announcement.created_at.desc())
    ).all()

    return {
        "terminal_name": "Pabama Bus Terminal Network",
        "current_time": now.isoformat(),
        "last_sync_timestamp": last_sync_time.isoformat(),
        "last_sync_minutes_ago": diff_minutes,
        "is_stale_warning": diff_minutes > 45, # HCI Heuristic: Visibility of System Status
        "available_buses_count": total_available,
        "regular_buses_count": regular_count,
        "twenty_four_hour_count": twenty_four_count,
        "announcements": [
            {
                "id": a.id,
                "title": a.title,
                "content": a.content,
                "priority": a.priority,
                "created_by": a.created_by_name,
                "created_at": a.created_at.isoformat()
            }
            for a in announcements
        ]
    }

@router.get("/trips")
def list_trips(
    origin: Optional[str] = Query(None, description="Starting point filter"),
    destination: Optional[str] = Query(None, description="Destination filter"),
    bus_type: Optional[str] = Query(None, description="REGULAR or 24_HOUR"),
    status: Optional[str] = Query(None, description="TripStatus filter"),
    session: Session = Depends(get_session)
):
    """List available and upcoming trips with origin/destination filters (Wireframes 1 & 2)."""
    now = datetime.now(timezone.utc)

    # Join Trip with Bus and Route
    statement = (
        select(Trip, Bus, Route)
        .join(Bus, Trip.bus_id == Bus.id)
        .join(Route, Trip.route_id == Route.id)
    )

    if origin:
        statement = statement.where(Route.origin.ilike(f"%{origin}%"))
    if destination:
        statement = statement.where(Route.destination.ilike(f"%{destination}%"))
    if bus_type:
        statement = statement.where(Trip.service_regime == bus_type)
    if status:
        statement = statement.where(Trip.status == status)
    else:
        # Exclude completed trips older than 2 hours by default
        statement = statement.where(Trip.status.in_([TripStatus.SCHEDULED, TripStatus.BOARDING, TripStatus.DEPARTED, TripStatus.ARRIVED]))

    statement = statement.order_by(Trip.last_updated_at.desc())
    results = session.exec(statement).all()

    trips_data = []
    for trip, bus, route in results:
        minutes_since_update = max(0, int((now - trip.last_updated_at).total_seconds() / 60))
        trips_data.append({
            "id": trip.id,
            "bus_number": bus.bus_number,
            "plate_number": bus.plate_number,
            "bus_type": bus.bus_type.value,
            "origin": route.origin,
            "destination": route.destination,
            "via_waypoints": route.via_waypoints,
            "estimated_duration_mins": route.estimated_duration_mins,
            "driver_name": trip.driver_name,
            "driver_license": trip.driver_license,
            "status": trip.status.value,
            "seat_status": trip.seat_status.value,
            "scheduled_time": trip.scheduled_time.isoformat() if trip.scheduled_time else None,
            "actual_departure": trip.actual_departure.isoformat() if trip.actual_departure else None,
            "actual_arrival": trip.actual_arrival.isoformat() if trip.actual_arrival else None,
            "last_updated_at": trip.last_updated_at.isoformat(),
            "minutes_since_update": minutes_since_update,
            "notes": trip.notes
        })

    return {
        "count": len(trips_data),
        "trips": trips_data
    }

@router.get("/trips/updates")
def get_movement_updates(
    limit: int = Query(15, ge=1, le=50),
    session: Session = Depends(get_session)
):
    """Chronological recent updates feed for arrivals and departures (Wireframe 3)."""
    now = datetime.now(timezone.utc)
    statement = (
        select(MovementLog, Bus, Staff)
        .join(Bus, MovementLog.bus_id == Bus.id)
        .join(Staff, MovementLog.staff_id == Staff.id)
        .order_by(MovementLog.recorded_at.desc())
        .limit(limit)
    )
    records = session.exec(statement).all()

    updates = []
    for log, bus, staff in records:
        minutes_ago = max(0, int((now - log.recorded_at).total_seconds() / 60))
        updates.append({
            "id": log.id,
            "movement_type": log.movement_type.value,
            "bus_number": bus.bus_number,
            "bus_type": bus.bus_type.value,
            "recorded_at": log.recorded_at.isoformat(),
            "recorded_time_str": log.recorded_at.strftime("%I:%M %p"),
            "minutes_ago": minutes_ago,
            "recorded_by": f"{staff.role.value.capitalize()} {staff.full_name}",
            "notes": log.notes,
            "is_correction": log.is_correction
        })

    return {"updates": updates}

@router.get("/routes")
def get_routes(session: Session = Depends(get_session)):
    """List available starting points and destinations for the search dropdowns."""
    routes = session.exec(select(Route).where(Route.is_active == True)).all()
    origins = sorted(list(set(r.origin for r in routes)))
    destinations = sorted(list(set(r.destination for r in routes)))
    return {
        "origins": origins,
        "destinations": destinations,
        "routes": [
            {
                "id": r.id,
                "origin": r.origin,
                "destination": r.destination,
                "via": r.via_waypoints
            }
            for r in routes
        ]
    }
