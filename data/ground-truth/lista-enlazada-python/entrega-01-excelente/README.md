# Lista Enlazada Simple (Python)

Implementación de una lista enlazada simple con inserción en ambos extremos,
eliminación por valor y búsqueda. No requiere dependencias externas (solo la
librería estándar).

## Uso

```python
from lista_enlazada import ListaEnlazada

lista = ListaEnlazada()
lista.insertar_final(1)
lista.insertar_final(2)
lista.insertar_inicio(0)
print(lista.a_lista())   # [0, 1, 2]
print(lista.buscar(2))   # True
lista.eliminar(1)
print(lista.a_lista())   # [0, 2]
```

## Pruebas

```bash
python -m unittest test_lista_enlazada.py
```

Las pruebas cubren las operaciones principales y los casos de borde: eliminar la
cabeza, eliminar la cola (verificando que la referencia al final se actualiza),
eliminar un valor inexistente y operar sobre una lista vacía.
