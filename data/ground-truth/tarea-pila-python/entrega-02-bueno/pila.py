"""Pila LIFO."""


class Pila:
    """Pila de elementos (LIFO)."""

    def __init__(self):
        self.elementos = []

    def apilar(self, elemento):
        self.elementos.append(elemento)

    def desapilar(self):
        if len(self.elementos) == 0:
            raise Exception("pila vacia")
        return self.elementos.pop()

    def tope(self):
        # Si la pila está vacía, esto lanza IndexError de la lista.
        return self.elementos[-1]

    def esta_vacia(self):
        return len(self.elementos) == 0

    def tamano(self):
        return len(self.elementos)
