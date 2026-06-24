"""Implementacion de una lista enlazada simple."""


class Nodo:
    """Nodo con un valor y referencia al siguiente."""

    def __init__(self, valor):
        self.valor = valor
        self.siguiente = None


class ListaEnlazada:
    """Lista enlazada simple con cabeza, cola y contador."""

    def __init__(self):
        self.cabeza = None
        self.cola = None
        self.cantidad = 0

    def esta_vacia(self):
        return self.cabeza is None

    def tamano(self):
        return self.cantidad

    def insertar_inicio(self, valor):
        """Agrega un valor al inicio de la lista."""
        nodo = Nodo(valor)
        nodo.siguiente = self.cabeza
        self.cabeza = nodo
        if self.cola is None:
            self.cola = nodo
        self.cantidad += 1

    def insertar_final(self, valor):
        """Agrega un valor al final de la lista."""
        nodo = Nodo(valor)
        if self.cabeza is None:
            self.cabeza = nodo
            self.cola = nodo
        else:
            self.cola.siguiente = nodo
            self.cola = nodo
        self.cantidad += 1

    def buscar(self, valor):
        actual = self.cabeza
        while actual is not None:
            if actual.valor == valor:
                return True
            actual = actual.siguiente
        return False

    def eliminar(self, valor):
        """Elimina la primera aparicion del valor, devuelve True/False."""
        anterior = None
        actual = self.cabeza
        while actual is not None:
            if actual.valor == valor:
                if anterior is None:
                    self.cabeza = actual.siguiente
                else:
                    anterior.siguiente = actual.siguiente
                if actual is self.cola:
                    self.cola = anterior
                self.cantidad -= 1
                return True
            anterior = actual
            actual = actual.siguiente
        return False

    def a_lista(self):
        salida = []
        actual = self.cabeza
        while actual is not None:
            salida.append(actual.valor)
            actual = actual.siguiente
        return salida
