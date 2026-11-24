from datetime import datetime, timezone, timedelta
from sqlmodel import Session, select
from app.database import engine
from app.models import (
    Bus, BusType, BusStatus, Route, Trip, TripStatus, SeatEstimate,
    Staff, StaffRole, Announcement, MovementLog, MovementType
)
from app.auth import hash_pin

def seed_initial_data():
    """Populates realistic Pabama bus routes, vehicles, staff, and announcements."""
    with Session(engine) as session:
        # Check if already seeded
        existing_bus = session.exec(select(Bus)).first()
        if existing_bus:
            return

        print("Seeding initial Pabama bus operational data...")

        # 1. Staff Members (Name + PIN)
        pin1_hash, pin1_salt = hash_pin("1234")
        inspector_juan = Staff(
            full_name="Juan Dela Cruz",
            role=StaffRole.INSPECTOR,
            contact_info="0917-123-4567",
            pin_hash=pin1_hash,
            pin_salt=pin1_salt
        )

        pin2_hash, pin2_salt = hash_pin("4321")
        dispatcher_maria = Staff(
            full_name="Maria Santos",
            role=StaffRole.DISPATCHER,
            contact_info="0918-987-6543",
            pin_hash=pin2_hash,
            pin_salt=pin2_salt
        )
        session.add(inspector_juan)
        session.add(dispatcher_maria)
        session.commit()
        session.refresh(inspector_juan)
        session.refresh(dispatcher_maria)

        # 2. Routes (Northern Mindanao Pabama corridors)
        r1 = Route(
            origin="Malaybalay Terminal",
            destination="Cagayan de Oro (Agora)",
            via_waypoints="Sayre Hwy, Impasug-ong, Manolo Fortich",
            distance_km=92.0,
            estimated_duration_mins=140
        )
        r2 = Route(
            origin="Cagayan de Oro (Agora)",
            destination="Malaybalay Terminal",
            via_waypoints="Manolo Fortich, Impasug-ong",
            distance_km=92.0,
            estimated_duration_mins=140
        )
        r3 = Route(
            origin="Malaybalay Terminal",
            destination="Valencia City Terminal",
            via_waypoints="Sayre Hwy, Bangcud, Mailag",
            distance_km=31.0,
            estimated_duration_mins=45
        )
        r4 = Route(
            origin="Valencia City Terminal",
            destination="Cagayan de Oro (Agora)",
            via_waypoints="Malaybalay, Manolo Fortich",
            distance_km=123.0,
            estimated_duration_mins=185
        )
        r5 = Route(
            origin="Cagayan de Oro (Agora)",
            destination="Kibawe Terminal",
            via_waypoints="Malaybalay, Valencia, Maramag",
            distance_km=175.0,
            estimated_duration_mins=260
        )
        routes = [r1, r2, r3, r4, r5]
        for r in routes:
            session.add(r)
        session.commit()
        for r in routes:
            session.refresh(r)

        # 3. Buses (Regular daytime ending at 8:00 PM vs. 24-Hour Night rotation)
        now = datetime.now(timezone.utc)
        buses = [
            # Regular Buses
            Bus(bus_number="Bus 101", plate_number="KBA-1001", bus_type=BusType.REGULAR, current_status=BusStatus.AT_TERMINAL, capacity=49, current_location="Malaybalay Terminal", last_updated_at=now - timedelta(minutes=10)),
            Bus(bus_number="Bus 102", plate_number="KBA-1002", bus_type=BusType.REGULAR, current_status=BusStatus.AT_TERMINAL, capacity=49, current_location="Malaybalay Terminal", last_updated_at=now - timedelta(minutes=25)),
            Bus(bus_number="Bus 104", plate_number="KBA-1004", bus_type=BusType.REGULAR, current_status=BusStatus.AT_TERMINAL, capacity=49, current_location="Malaybalay Terminal", last_updated_at=now - timedelta(minutes=5)),
            Bus(bus_number="Bus 105", plate_number="KBA-1005", bus_type=BusType.REGULAR, current_status=BusStatus.EN_ROUTE, capacity=49, current_location="Sayre Hwy (Manolo Fortich)", last_updated_at=now - timedelta(minutes=40)),
            Bus(bus_number="Bus 108", plate_number="KBA-1008", bus_type=BusType.REGULAR, current_status=BusStatus.AT_TERMINAL, capacity=49, current_location="Malaybalay Terminal", last_updated_at=now - timedelta(minutes=15)),

            # 24-Hour Buses (Active late night based on driver presence/handoffs)
            Bus(bus_number="Bus 201", plate_number="KBA-2001", bus_type=BusType.TWENTY_FOUR_HOUR, current_status=BusStatus.AT_TERMINAL, capacity=49, current_location="Malaybalay Terminal", last_updated_at=now - timedelta(minutes=8)),
            Bus(bus_number="Bus 203", plate_number="KBA-2003", bus_type=BusType.TWENTY_FOUR_HOUR, current_status=BusStatus.AT_TERMINAL, capacity=49, current_location="Malaybalay Terminal", last_updated_at=now - timedelta(minutes=30)),
            Bus(bus_number="Bus 204", plate_number="KBA-2004", bus_type=BusType.TWENTY_FOUR_HOUR, current_status=BusStatus.EN_ROUTE, capacity=49, current_location="Sayre Hwy (Alae)", last_updated_at=now - timedelta(minutes=55)),
            Bus(bus_number="Bus 207", plate_number="KBA-2007", bus_type=BusType.TWENTY_FOUR_HOUR, current_status=BusStatus.AT_TERMINAL, capacity=49, current_location="Malaybalay Terminal", last_updated_at=now - timedelta(minutes=12)),
        ]
        for b in buses:
            session.add(b)
        session.commit()
        for b in buses:
            session.refresh(b)

        # 4. Announcements (Digitizing walk-around megaphone notices)
        a1 = Announcement(
            title="Terminal Night Operation Schedule",
            content="Regular scheduled trips end at 8:00 PM. 24-Hour buses operate continuously based on vehicle arrival handoffs. Inquire directly with terminal inspector for live boarding.",
            priority="NORMAL",
            created_at=now - timedelta(hours=2),
            created_by_name="Inspector Juan Dela Cruz"
        )
        a2 = Announcement(
            title="Bay 2 Dispatch Update",
            content="All direct trips to Cagayan de Oro (Agora) will stage at Platform 2. Please have exact fare ready for conductor.",
            priority="NORMAL",
            created_at=now - timedelta(minutes=45),
            created_by_name="Dispatcher Maria Santos"
        )
        session.add(a1)
        session.add(a2)

        # 5. Active Trips & Movement Logs
        t1 = Trip(
            bus_id=buses[2].id, # Bus 104
            route_id=r1.id,     # to Cagayan
            driver_name="Arnel Pimentel",
            driver_license="D02-12-004312",
            service_regime=BusType.REGULAR,
            status=TripStatus.BOARDING,
            seat_status=SeatEstimate.AVAILABLE,
            scheduled_time=now + timedelta(minutes=20),
            last_updated_at=now - timedelta(minutes=5),
            notes="Boarding at Platform 2"
        )
        session.add(t1)
        session.commit()
        session.refresh(t1)

        log1 = MovementLog(
            trip_id=t1.id,
            bus_id=buses[2].id,
            staff_id=inspector_juan.id,
            movement_type=MovementType.ARRIVAL,
            recorded_at=now - timedelta(minutes=35),
            notes="Arrived from Valencia; turned around for Cagayan run"
        )
        session.add(log1)

        t2 = Trip(
            bus_id=buses[5].id, # Bus 201
            route_id=r1.id,
            driver_name="Rodrigo Baclayon",
            driver_license="D01-08-998811",
            service_regime=BusType.TWENTY_FOUR_HOUR,
            status=TripStatus.SCHEDULED,
            seat_status=SeatEstimate.AVAILABLE,
            scheduled_time=now + timedelta(minutes=60),
            last_updated_at=now - timedelta(minutes=8),
            notes="24-Hour Bus standby"
        )
        session.add(t2)
        session.commit()
        session.refresh(t2)

        log2 = MovementLog(
            trip_id=t2.id,
            bus_id=buses[5].id,
            staff_id=inspector_juan.id,
            movement_type=MovementType.ARRIVAL,
            recorded_at=now - timedelta(minutes=40),
            notes="24-Hour vehicle checked and fueled"
        )
        session.add(log2)

        t3 = Trip(
            bus_id=buses[3].id, # Bus 105
            route_id=r3.id,
            driver_name="Danilo Tan",
            driver_license="D04-15-554422",
            service_regime=BusType.REGULAR,
            status=TripStatus.DEPARTED,
            seat_status=SeatEstimate.FULL_LOAD,
            actual_departure=now - timedelta(minutes=40),
            last_updated_at=now - timedelta(minutes=40),
            notes="Departed for Valencia"
        )
        session.add(t3)
        session.commit()
        session.refresh(t3)

        log3 = MovementLog(
            trip_id=t3.id,
            bus_id=buses[3].id,
            staff_id=dispatcher_maria.id,
            movement_type=MovementType.DEPARTURE,
            recorded_at=now - timedelta(minutes=40),
            notes="Full load departed on time"
        )
        session.add(log3)

        session.commit()
        print("Pabama bus operational data successfully seeded!")
