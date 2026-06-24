class Nodo:
    def __init__(self, valor):
        self.valor = valor
        self.siguiente = None


class ListaEnlazada:
    def __init__(self):
        self.cabeza = None
        self.n = 0

    def insertar_final(self, valor):
        nodo = Nodo(valor)
        nodo.siguiente = self.cabeza
        self.cabeza = nodo
        self.n += 1

    def eliminar(self, valor):
        self.cabeza = self.cabeza.siguiente

    def buscar(self, valor):
        actual = self.cabeza
        if actual is not None and actual.valor == valor:
            return True
        return False

    def tamano(self):
        return self.n

    def a_lista(self):
        salida = []
        actual = self.cabeza
        while actual is not None:
            salida.append(actual.valor)
            actual = actual.siguiente
        return salida
