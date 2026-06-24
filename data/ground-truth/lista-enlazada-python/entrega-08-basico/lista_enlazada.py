class Nodo:
    def __init__(self, valor):
        self.valor = valor
        self.siguiente = None


class ListaEnlazada:
    def __init__(self):
        self.cabeza = None

    def esta_vacia(self):
        return self.cabeza is None

    def insertar_inicio(self, valor):
        nodo = Nodo(valor)
        nodo.siguiente = self.cabeza
        self.cabeza = nodo

    def insertar_final(self, valor):
        nodo = Nodo(valor)
        if self.cabeza is None:
            self.cabeza = nodo
            return
        actual = self.cabeza
        while actual.siguiente is not None:
            actual = actual.siguiente
        actual.siguiente = nodo

    def eliminar(self, valor):
        anterior = None
        actual = self.cabeza
        while actual is not None:
            if actual.valor == valor:
                if anterior is None:
                    self.cabeza = actual.siguiente
                else:
                    anterior.siguiente = actual.siguiente
                return
            anterior = actual
            actual = actual.siguiente

    def a_lista(self):
        salida = []
        actual = self.cabeza
        while actual is not None:
            salida.append(actual.valor)
            actual = actual.siguiente
        return salida
