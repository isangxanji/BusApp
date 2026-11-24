import os
import sys
import socket
import uvicorn

# Ensure the backend directory is in the Python search path
current_dir = os.path.dirname(os.path.abspath(__file__))
backend_dir = os.path.join(current_dir, "backend")
if backend_dir not in sys.path:
    sys.path.insert(0, backend_dir)

def get_local_ip():
    """Finds the local network IP address of this machine for phone testing."""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"

def print_welcome_banner(local_ip, port=8000):
    print("=" * 64)
    print("    🚌  PABAMA BUS INFO SYS - EXHIBIT RUNTIME SERVER  🚌")
    print("=" * 64)
    print(f"  • Desktop Browser / Exhibit Laptop:  http://localhost:{port}")
    print(f"  • Real Mobile Smartphone (Same WiFi): http://{local_ip}:{port}")
    print("-" * 64)
    print("  EXHIBIT DEMO TIPS:")
    print("  1. Evaluators can open the link directly on their phones.")
    print("  2. To experience fullscreen PWA, tap 'Add to Home Screen' on mobile.")
    print("  3. Staff test credentials:")
    print("     - Inspector:  Juan Dela Cruz  (PIN: 1234)")
    print("     - Dispatcher: Maria Santos    (PIN: 4321)")
    print("=" * 64)
    print("Starting server... Press CTRL+C to stop.\n")

if __name__ == "__main__":
    local_ip = get_local_ip()
    port = 8000
    print_welcome_banner(local_ip, port)
    uvicorn.run("app.main:app", host="0.0.0.0", port=port, reload=True)
