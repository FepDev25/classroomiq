#!/usr/bin/env python3
"""Genera los directorios de entrega y los .zip listos para subir a partir de _entregas.bundle.

Cada entrega es un directorio de archivos fuente; el .zip correspondiente contiene
esos archivos en la raiz del archivo (sin carpeta contenedora), igual que el set
tarea-pila-python. Reproducible: borra y regenera entrega-* y zips/ en cada corrida.
"""
import os
import shutil
import zipfile

BASE = os.path.dirname(os.path.abspath(__file__))
BUNDLE = os.path.join(BASE, "_entregas.bundle")
ZIPS = os.path.join(BASE, "zips")
MARCA = "===FILE=== "

# Nombre de zip (estudiante ficticio) por directorio de entrega.
ZIP_POR_DIR = {
    "entrega-01-excelente": "01-ana-torres",
    "entrega-02-excelente": "02-bruno-mendez",
    "entrega-03-bueno": "03-carla-rios",
    "entrega-04-bueno": "04-diego-fuentes",
    "entrega-05-bueno": "05-elena-paredes",
    "entrega-06-basico": "06-fabian-soto",
    "entrega-07-basico": "07-gabriela-nunez",
    "entrega-08-basico": "08-hugo-ramirez",
    "entrega-09-insuficiente": "09-ines-cardenas",
    "entrega-10-insuficiente": "10-javier-ortega",
    "entrega-11-bueno": "11-karina-lucero",
    "entrega-12-excelente": "12-luis-bravo",
    "entrega-13-basico": "13-maria-salas",
    "entrega-14-insuficiente": "14-nicolas-vega",
    "entrega-15-bueno": "15-paula-herrera",
}


def parse_bundle(path):
    """Devuelve {ruta_relativa: contenido} a partir del bundle."""
    archivos = {}
    actual = None
    lineas = []
    with open(path, encoding="utf-8") as fh:
        for linea in fh:
            if linea.startswith(MARCA):
                if actual is not None:
                    archivos[actual] = "".join(lineas)
                actual = linea[len(MARCA):].strip()
                lineas = []
            else:
                lineas.append(linea)
    if actual is not None:
        archivos[actual] = "".join(lineas)
    return archivos


def main():
    archivos = parse_bundle(BUNDLE)

    # limpia salidas previas
    for d in ZIP_POR_DIR:
        ruta = os.path.join(BASE, d)
        if os.path.isdir(ruta):
            shutil.rmtree(ruta)
    if os.path.isdir(ZIPS):
        shutil.rmtree(ZIPS)
    os.makedirs(ZIPS)

    # escribe los archivos fuente
    for rel, contenido in archivos.items():
        destino = os.path.join(BASE, rel)
        os.makedirs(os.path.dirname(destino), exist_ok=True)
        with open(destino, "w", encoding="utf-8") as fh:
            fh.write(contenido)

    # arma un zip por entrega (archivos en la raiz del zip)
    for entrega, zipname in ZIP_POR_DIR.items():
        dir_entrega = os.path.join(BASE, entrega)
        archivos_entrega = sorted(os.listdir(dir_entrega))
        ruta_zip = os.path.join(ZIPS, zipname + ".zip")
        with zipfile.ZipFile(ruta_zip, "w", zipfile.ZIP_DEFLATED) as zf:
            for nombre in archivos_entrega:
                zf.write(os.path.join(dir_entrega, nombre), arcname=nombre)
        print(f"{entrega:28s} -> zips/{zipname}.zip ({len(archivos_entrega)} archivos)")

    print(f"\nTotal: {len(ZIP_POR_DIR)} entregas, {len(archivos)} archivos fuente.")


if __name__ == "__main__":
    main()
