# Lista Enlazada en Python

Implementa una lista enlazada simple con inserción al inicio y al final,
búsqueda y eliminación de un valor.

## Uso

```python
from lista_enlazada import ListaEnlazada

lista = ListaEnlazada()
lista.insertar_final(1)
lista.insertar_final(2)
print(lista.a_lista())  # [1, 2]
lista.eliminar(1)
print(lista.a_lista())  # [2]
```

## Pruebas

```bash
python -m unittest test_lista_enlazada.py
```

Nota: `eliminar` lanza `ValueError` si el valor no está en la lista.
