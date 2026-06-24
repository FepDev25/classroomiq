"""Lista enlazada simple."""


class Nodo:
    def __init__(self, valor):
        self.valor = valor
        self.siguiente = None


class ListaEnlazada:
    def __init__(self):
        self.cabeza = None

    def esta_vacia(self):
        return self.cabeza is None

    def tamano(self):
        n = 0
        actual = self.cabeza
        while actual is not None:
            n += 1
            actual = actual.siguiente
        return n

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

    def buscar(self, valor):
        actual = self.cabeza
        while actual is not None:
            if actual.valor == valor:
                return True
            actual = actual.siguiente
        return False

    def eliminar(self, valor):
        # busca el valor y enlaza el anterior con el siguiente
        anterior = self.cabeza
        actual = self.cabeza
        while actual is not None:
            if actual.valor == valor:
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
