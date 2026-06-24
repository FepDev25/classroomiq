"""Lista enlazada simple."""


class Nodo:
    def __init__(self, valor):
        self.valor = valor
        self.siguiente = None


class ListaEnlazada:
    """Lista enlazada con inicio y fin."""

    def __init__(self):
        self.inicio = None
        self.fin = None
        self.n = 0

    def esta_vacia(self):
        return self.inicio is None

    def tamano(self):
        return self.n

    def insertar_inicio(self, valor):
        nodo = Nodo(valor)
        nodo.siguiente = self.inicio
        self.inicio = nodo
        if self.fin is None:
            self.fin = nodo
        self.n += 1

    def insertar_final(self, valor):
        nodo = Nodo(valor)
        if self.inicio is None:
            self.inicio = nodo
            self.fin = nodo
        else:
            self.fin.siguiente = nodo
            self.fin = nodo
        self.n += 1

    def buscar(self, valor):
        actual = self.inicio
        while actual is not None:
            if actual.valor == valor:
                return True
            actual = actual.siguiente
        return False

    def eliminar(self, valor):
        anterior = None
        actual = self.inicio
        while actual is not None:
            if actual.valor == valor:
                if anterior is None:
                    self.inicio = actual.siguiente
                else:
                    anterior.siguiente = actual.siguiente
                self.n -= 1
                return
            anterior = actual
            actual = actual.siguiente

    def a_lista(self):
        salida = []
        actual = self.inicio
        while actual is not None:
            salida.append(actual.valor)
            actual = actual.siguiente
        return salida
