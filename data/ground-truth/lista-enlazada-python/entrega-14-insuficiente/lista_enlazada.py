class Nodo:
    def __init__(self, v):
        self.v = v
        self.sig = None


class ListaEnlazada:
    def __init__(self):
        self.cabeza = None

    def insertar(self, v):
        nodo = Nodo(v)
        nodo.sig = self.cabeza
        self.cabeza = nodo

    def eliminar(self, v):
        actual = self.cabeza
        while actual is not None:
            if actual.v == v:
                actual = actual.sig
            actual = actual.sig

    def mostrar(self):
        actual = self.cabeza
        while actual:
            print(actual.v)
            actual = actual.sig
