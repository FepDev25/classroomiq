"""Estructura de datos Pila (LIFO) implementada sobre una lista de Python."""


class PilaVaciaError(Exception):
    """Se intentó desapilar o consultar el tope de una pila sin elementos."""


class Pila:
    """Pila de elementos con política LIFO (último en entrar, primero en salir)."""

    def __init__(self):
        self._elementos = []

    def apilar(self, elemento):
        """Coloca un elemento en el tope de la pila."""
        self._elementos.append(elemento)

    def desapilar(self):
        """Quita y devuelve el elemento del tope.

        Lanza PilaVaciaError si la pila está vacía.
        """
        if self.esta_vacia():
            raise PilaVaciaError("No se puede desapilar: la pila está vacía")
        return self._elementos.pop()

    def tope(self):
        """Devuelve el elemento del tope sin quitarlo.

        Lanza PilaVaciaError si la pila está vacía.
        """
        if self.esta_vacia():
            raise PilaVaciaError("No se puede consultar el tope: la pila está vacía")
        return self._elementos[-1]

    def esta_vacia(self):
        """Indica si la pila no tiene elementos."""
        return len(self._elementos) == 0

    def tamano(self):
        """Cantidad de elementos en la pila."""
        return len(self._elementos)
