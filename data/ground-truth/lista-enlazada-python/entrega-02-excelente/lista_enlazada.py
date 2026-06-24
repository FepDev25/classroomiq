"""Lista enlazada simple en Python."""


class Nodo:
    """Nodo de la lista: un valor y el enlace al siguiente nodo."""

    def __init__(self, valor):
        self.valor = valor
        self.siguiente = None


class ListaEnlazada:
    """Lista enlazada simple con inserción, eliminación y búsqueda por valor."""

    def __init__(self):
        self.cabeza = None

    def esta_vacia(self):
        """Indica si la lista no tiene elementos."""
        return self.cabeza is None

    def tamano(self):
        """Cuenta los elementos recorriendo la lista."""
        total = 0
        actual = self.cabeza
        while actual is not None:
            total += 1
            actual = actual.siguiente
        return total

    def insertar_inicio(self, valor):
        """Inserta un elemento al principio."""
        nodo = Nodo(valor)
        nodo.siguiente = self.cabeza
        self.cabeza = nodo

    def insertar_final(self, valor):
        """Inserta un elemento al final recorriendo hasta el último nodo."""
        nodo = Nodo(valor)
        if self.cabeza is None:
            self.cabeza = nodo
            return
        actual = self.cabeza
        while actual.siguiente is not None:
            actual = actual.siguiente
        actual.siguiente = nodo

    def buscar(self, valor):
        """Devuelve True si el valor está en la lista."""
        actual = self.cabeza
        while actual is not None:
            if actual.valor == valor:
                return True
            actual = actual.siguiente
        return False

    def eliminar(self, valor):
        """Elimina la primera aparición del valor. Devuelve True si lo encontró."""
        anterior = None
        actual = self.cabeza
        while actual is not None:
            if actual.valor == valor:
                if anterior is None:
                    self.cabeza = actual.siguiente
                else:
                    anterior.siguiente = actual.siguiente
                return True
            anterior = actual
            actual = actual.siguiente
        return False

    def a_lista(self):
        """Devuelve los elementos en una lista de Python."""
        salida = []
        actual = self.cabeza
        while actual is not None:
            salida.append(actual.valor)
            actual = actual.siguiente
        return salida
