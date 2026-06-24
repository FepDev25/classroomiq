"""Estructura de lista enlazada simple que guarda una sucesion de valores."""


class Celda:
    """Celda de la lista: un valor y el enlace a la celda siguiente."""

    def __init__(self, valor):
        self.valor = valor
        self.siguiente = None


class ListaEnlazada:
    """Lista enlazada con referencias al primero y al ultimo elemento."""

    def __init__(self):
        self.primero = None
        self.ultimo = None
        self.cantidad = 0

    def esta_vacia(self):
        return self.primero is None

    def tamano(self):
        return self.cantidad

    def insertar_inicio(self, valor):
        celda = Celda(valor)
        celda.siguiente = self.primero
        self.primero = celda
        if self.ultimo is None:
            self.ultimo = celda
        self.cantidad += 1

    def insertar_final(self, valor):
        celda = Celda(valor)
        if self.primero is None:
            self.primero = celda
            self.ultimo = celda
        else:
            self.ultimo.siguiente = celda
            self.ultimo = celda
        self.cantidad += 1

    def buscar(self, valor):
        recorrido = self.primero
        while recorrido is not None:
            if recorrido.valor == valor:
                return True
            recorrido = recorrido.siguiente
        return False

    def eliminar(self, valor):
        previo = None
        recorrido = self.primero
        while recorrido is not None:
            if recorrido.valor == valor:
                if previo is None:
                    self.primero = recorrido.siguiente
                else:
                    previo.siguiente = recorrido.siguiente
                self.cantidad -= 1
                return True
            previo = recorrido
            recorrido = recorrido.siguiente
        return False

    def a_lista(self):
        salida = []
        recorrido = self.primero
        while recorrido is not None:
            salida.append(recorrido.valor)
            recorrido = recorrido.siguiente
        return salida
