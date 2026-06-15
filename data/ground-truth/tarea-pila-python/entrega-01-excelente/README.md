# Pila (LIFO) en Python

Implementación de una estructura de datos Pila con política LIFO sobre una lista.

## Ejecución

No requiere dependencias externas (solo la librería estándar).

```python
from pila import Pila

p = Pila()
p.apilar(1)
p.apilar(2)
print(p.tope())      # 2
print(p.desapilar()) # 2
print(p.tamano())    # 1
```

## Pruebas

```bash
python -m unittest test_pila.py
```

Las pruebas cubren las operaciones principales y los casos de borde (desapilar y
consultar el tope sobre una pila vacía, que lanzan `PilaVaciaError`).
