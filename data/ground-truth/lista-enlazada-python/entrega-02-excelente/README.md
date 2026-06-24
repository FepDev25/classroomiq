# Lista Enlazada Simple

Lista enlazada simple en Python con inserción al inicio y al final, búsqueda y
eliminación por valor.

## Ejemplo de uso

```python
from lista_enlazada import ListaEnlazada

lista = ListaEnlazada()
lista.insertar_final("a")
lista.insertar_final("b")
print(lista.a_lista())  # ['a', 'b']
lista.eliminar("a")
print(lista.a_lista())  # ['b']
```

## Cómo correr las pruebas

```bash
python -m unittest test_lista_enlazada.py
```
