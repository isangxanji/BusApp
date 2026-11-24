from datetime import datetime, timezone
from enum import Enum
from typing import Optional, List
from sqlmodel import SQLModel, Field, Relationship

def utc_now() -> datetime:
    return datetime.now(timezone.utc)

class BusType(str, Enum):
    REGULAR = "REGULAR"                 # Operates on daytime schedule ending ~8:00 PM
    TWENTY_FOUR_HOUR = "24_HOUR"        # Night transit based on vehicle presence and handoffs

class BusStatus(str, Enum):
    AT_TERMINAL = "AT_TERMINAL"
    EN_ROUTE = "EN_ROUTE"
    MAINTENANCE = "MAINTENANCE"

class TripStatus(str, Enum):
    SCHEDULED = "SCHEDULED"
    BOARDING = "BOARDING"
    DEPARTED = "DEPARTED"
    ARRIVED = "ARRIVED"
    CANCELLED = "CANCELLED"

class SeatEstimate(str, Enum):
    AVAILABLE = "AVAILABLE"             # Plentiful seats recorded
    FEW_SEATS = "FEW_SEATS"             # Limited seats remaining
    FULL_LOAD = "FULL_LOAD"             # All seats occupied
    STANDING_ONLY = "STANDING_ONLY"     # Standing room only permitted

class MovementType(str, Enum):
    ARRIVAL = "ARRIVAL"
    DEPARTURE = "DEPARTURE"

class StaffRole(str, Enum):
    INSPECTOR = "INSPECTOR"
    DISPATCHER = "DISPATCHER"
    CONDUCTOR = "CONDUCTOR"

# Database Models
class Staff(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    full_name: str = Field(index=True)
    role: StaffRole = Field(default=StaffRole.INSPECTOR)
    contact_info: str                   # Phone number or email
    pin_hash: str                       # Salted PBKDF2 hash of 4-6 digit PIN
    pin_salt: str
    is_active: bool = Field(default=True)
    created_at: datetime = Field(default_factory=utc_now)

class Bus(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    bus_number: str = Field(index=True, unique=True)    # e.g., "Bus 104"
    plate_number: str                                   # e.g., "KBA-1234"
    bus_type: BusType = Field(default=BusType.REGULAR)
    current_status: BusStatus = Field(default=BusStatus.AT_TERMINAL)
    capacity: int = Field(default=49)
    current_location: str = Field(default="Agora Terminal, CDO")
    last_updated_at: datetime = Field(default_factory=utc_now)

class Route(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    origin: str = Field(index=True)                     # e.g., "Cagayan de Oro"
    destination: str = Field(index=True)                # e.g., "Malaybalay City"
    via_waypoints: Optional[str] = None                 # e.g., "Sayre Hwy, Manolo Fortich, Impasug-ong"
    distance_km: float = Field(default=95.0)
    estimated_duration_mins: int = Field(default=150)
    is_active: bool = Field(default=True)

class Trip(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    bus_id: int = Field(foreign_key="bus.id", index=True)
    route_id: int = Field(foreign_key="route.id", index=True)
    driver_name: str
    driver_license: str
    service_regime: BusType = Field(default=BusType.REGULAR)
    status: TripStatus = Field(default=TripStatus.SCHEDULED)
    seat_status: SeatEstimate = Field(default=SeatEstimate.AVAILABLE)
    scheduled_time: Optional[datetime] = None
    actual_departure: Optional[datetime] = None
    actual_arrival: Optional[datetime] = None
    last_updated_at: datetime = Field(default_factory=utc_now)
    notes: Optional[str] = None

class MovementLog(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    trip_id: Optional[int] = Field(default=None, foreign_key="trip.id")
    bus_id: int = Field(foreign_key="bus.id", index=True)
    staff_id: int = Field(foreign_key="staff.id")
    movement_type: MovementType
    recorded_at: datetime = Field(default_factory=utc_now)
    notes: Optional[str] = None
    is_correction: bool = Field(default=False)
    original_log_id: Optional[int] = None

class Announcement(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    title: str
    content: str
    priority: str = Field(default="NORMAL") # NORMAL, URGENT
    created_at: datetime = Field(default_factory=utc_now)
    created_by_name: str = Field(default="Terminal Dispatcher")
    is_active: bool = Field(default=True)
