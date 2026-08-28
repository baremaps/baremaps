# Licensed under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import argparse
from functools import partial
import multiprocessing
import subprocess
from pathlib import Path
import tqdm


def polygonize(raster_path: Path, out_path: Path):
    """Polygonize a raster file using gdal_polygonize.py

    Args:
        raster_path (Path): Path to raster file
        out_path (Path): Path to output folder

    Raises:
        subprocess.CalledProcessError: If the command fails
    """
    dst_path = out_path / raster_path.name.replace(".tif", ".gpkg")
    # This condition is to avoid overwriting existing files
    # Note: If the script is interrupted, unfinished files will be left unfinished. To
    #       avoid this, delete the unfinished files by checking the last time modified and run the script again.
    if not dst_path.exists():
        command = [
            "gdal_polygonize.py",
            str(raster_path),
            "-b",
            "1",
            "-f",
            '"GPKG"',
            str(dst_path),
            raster_path.name.replace(".tif", ""),
            "DN",
        ]
        subprocess.run(
            " ".join(command),
            shell=True,
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )


def main():
    """Main function"""
    parser = argparse.ArgumentParser(
        description="Batch polygonize raster files using gdal_polygonize.py"
    )
    parser.add_argument(
        "-p", "--path", help="Path to raster files", type=str, required=True
    )
    parser.add_argument(
        "-o", "--out", help="Path to output folder", type=str, required=True
    )
    args = parser.parse_args()

    raster_path = Path(args.path)
    out_path = Path(args.out)

    paths = raster_path.rglob("*.tif")
    total = len(list(raster_path.rglob("*.tif")))

    print(f"Found a total of {total} raster files.")

    with multiprocessing.Pool(processes=multiprocessing.cpu_count()) as pool:
        for _ in tqdm.tqdm(
            pool.imap_unordered(partial(polygonize, out_path=out_path), paths),
            total=total,
        ):
            pass


if __name__ == "__main__":
    main()
