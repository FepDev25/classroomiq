"""Lista enlazada simple para almacenar una secuencia de valores."""


class Nodo:
    """Nodo de la lista: un dato y el enlace al nodo siguiente."""

    def __init__(self, dato):
        self.dato = dato
        self.siguiente = None


class ListaEnlazada:
    """Lista enlazada con cabeza y cola para insertar en ambos extremos."""

    def __init__(self):
        self.cabeza = None
        self.cola = None
        self.longitud = 0

    def esta_vacia(self):
        return self.cabeza is None

    def tamano(self):
        return self.longitud

    def insertar_inicio(self, dato):
        nodo = Nodo(dato)
        nodo.siguiente = self.cabeza
        self.cabeza = nodo
        if self.cola is None:
            self.cola = nodo
        self.longitud += 1

    def insertar_final(self, dato):
        nodo = Nodo(dato)
        if self.cabeza is None:
            self.cabeza = nodo
            self.cola = nodo
        else:
            self.cola.siguiente = nodo
            self.cola = nodo
        self.longitud += 1

    def buscar(self, dato):
        actual = self.cabeza
        while actual is not None:
            if actual.dato == dato:
                return True
            actual = actual.siguiente
        return False

    def eliminar(self, dato):
        anterior = None
        actual = self.cabeza
        while actual is not None:
            if actual.dato == dato:
                if anterior is None:
                    self.cabeza = actual.siguiente
                else:
                    anterior.siguiente = actual.siguiente
                self.longitud -= 1
                return True
            anterior = actual
            actual = actual.siguiente
        return False

    def a_lista(self):
        resultado = []
        actual = self.cabeza
        while actual is not None:
            resultado.append(actual.dato)
            actual = actual.siguiente
        return resultado
