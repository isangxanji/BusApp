import hashlib
import secrets
from typing import Tuple, Optional

def hash_pin(pin: str) -> Tuple[str, str]:
    """Hashes a PIN using PBKDF2-HMAC-SHA256 with a unique random salt."""
    salt = secrets.token_hex(16)
    pin_hash = hashlib.pbkdf2_hmac(
        'sha256',
        pin.encode('utf-8'),
        salt.encode('utf-8'),
        100000
    ).hex()
    return pin_hash, salt

def verify_pin(pin: str, stored_hash: str, salt: str) -> bool:
    """Verifies a plain text PIN against a stored hash and salt."""
    computed_hash = hashlib.pbkdf2_hmac(
        'sha256',
        pin.encode('utf-8'),
        salt.encode('utf-8'),
        100000
    ).hex()
    return secrets.compare_digest(computed_hash, stored_hash)

# Simple fast shift session token generator
def generate_session_token(staff_id: int, full_name: str) -> str:
    random_part = secrets.token_urlsafe(24)
    return f"pabama_staff_{staff_id}_{random_part}"
