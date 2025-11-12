import neurokit2 as nk
import numpy as np
import time
from datetime import datetime, timezone
import yaml
from pathlib import Path
import socket


def load_cfg(path: str | Path = "config.yml") -> dict:
    p = Path(path)
    if p.is_file():
        with p.open() as f:
            return yaml.safe_load(f) or {}
    return {}


def generate_data():
    fps = 50
    sample_rate = 180
    x_window = 3  # seconds
    slide_duration = 1  # seconds

    simulated_ecg = nk.ecg_simulate(duration=8, sampling_rate=200, heart_rate=80)
    simulated_ecg = nk.ecg_simulate(
        duration=2,
        sampling_rate=sample_rate,
        method="daubechies",
        heart_rate=60,
        heart_rate_std=0,
    )

    tile = simulated_ecg[0:sample_rate]
    tile_count = (x_window * sample_rate) + (slide_duration * sample_rate)
    simulated_ecg = np.tile(tile, tile_count)
    print(np.shape(simulated_ecg))
    return simulated_ecg


def main() -> None:
    cfg = yaml.safe_load(Path("config.yml").read_text()) or {}
    ecg_cfg = cfg.get("ecg", {})

    BIND_HOST = ecg_cfg.get("broker", "0.0.0.0")
    TCP_PORT = ecg_cfg.get("port", 5000)
    PUBLISH_PERIOD = ecg_cfg.get("period", 30)  # ms

    data = generate_data()
    index = 0

    server_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server_sock.bind((BIND_HOST, TCP_PORT))
    server_sock.listen(1)

    print(f"{BIND_HOST}:{TCP_PORT}")

    try:
        while True:
            conn, addr = server_sock.accept()
            print(f"{addr} connected")

            try:
                while True:
                    timestamp = int(
                        datetime.now(timezone.utc).timestamp() * 1_000
                    )
                    csv_row = f"{timestamp},{data[index]}"
                    conn.sendall(csv_row.encode("utf-8"))
                    index = (index + 1) % len(data)
                    time.sleep(PUBLISH_PERIOD / 1000.0)
            except (BrokenPipeError, ConnectionResetError):
                print("disconnected")
                conn.close()
            except KeyboardInterrupt:
                conn.close()
                break

    except KeyboardInterrupt:
        print("\nstopping")
    finally:
        server_sock.close()


if __name__ == "__main__":
    main()
