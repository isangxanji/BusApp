from sqlmodel import SQLModel, create_engine, Session
import os

DB_FILE = os.getenv("PABAMA_DB_FILE", "pabama_bus.db")
DATABASE_URL = f"sqlite:///{DB_FILE}"

# check_same_thread=False allows FastAPI multi-threaded requests with SQLite
engine = create_engine(
    DATABASE_URL,
    connect_args={"check_same_thread": False},
    echo=False
)

def create_db_and_tables():
    """Initializes the SQLite database tables."""
    SQLModel.metadata.create_all(engine)

def get_session():
    """FastAPI dependency for yielding database sessions."""
    with Session(engine) as session:
        yield session
