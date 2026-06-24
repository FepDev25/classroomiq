# Lista Enlazada Simple

Estructura de datos de lista enlazada simple implementada en Python, con celdas
enlazadas y referencias al primero y al último elemento.

## Ejemplo

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
