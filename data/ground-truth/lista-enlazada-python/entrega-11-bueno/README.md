# Lista Enlazada

Lista enlazada simple en Python con nodos enlazados y referencias a la cabeza y
a la cola.

```python
from lista_enlazada import ListaEnlazada

lista = ListaEnlazada()
lista.insertar_final(1)
lista.insertar_final(2)
lista.eliminar(1)
print(lista.a_lista())  # [2]
```
