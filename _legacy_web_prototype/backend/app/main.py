from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
import os

from app.database import create_db_and_tables
from app.seed import seed_initial_data
from app.routers import passenger, staff

# Ensure tables and seed data exist immediately
create_db_and_tables()
seed_initial_data()

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Ensure fresh state on server launch
    create_db_and_tables()
    seed_initial_data()
    yield

app = FastAPI(
    title="Pabama Bus Info Sys - Backend API",
    description="Software backend for the Pabama Bus Information and Departure System (HCI Term Project)",
    version="1.0.0",
    lifespan=lifespan
)

# Enable CORS for local Wi-Fi and mobile device testing
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include Backend API Routers
app.include_router(passenger.router)
app.include_router(staff.router)

@app.get("/api/health")
def health_check():
    return {"status": "ok", "app": "Pabama Bus Info Sys", "version": "1.0.0"}

# Mount frontend directory for direct mobile web serving
frontend_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "frontend"))
if os.path.exists(frontend_dir):
    app.mount("/", StaticFiles(directory=frontend_dir, html=True), name="frontend")
