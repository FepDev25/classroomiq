"""Lista enlazada simple con busqueda y eliminacion por valor."""


class Nodo:
    """Nodo de la lista enlazada."""

    def __init__(self, valor):
        self.valor = valor
        self.siguiente = None


class ListaEnlazada:
    """Lista enlazada simple (solo referencia a la cabeza)."""

    def __init__(self):
        self.cabeza = None

    def esta_vacia(self):
        """True si no hay elementos."""
        return self.cabeza is None

    def tamano(self):
        """Numero de elementos."""
        n = 0
        actual = self.cabeza
        while actual is not None:
            n += 1
            actual = actual.siguiente
        return n

    def insertar_inicio(self, valor):
        """Inserta al inicio."""
        nodo = Nodo(valor)
        nodo.siguiente = self.cabeza
        self.cabeza = nodo

    def insertar_final(self, valor):
        """Inserta al final."""
        nodo = Nodo(valor)
        if self.cabeza is None:
            self.cabeza = nodo
            return
        actual = self.cabeza
        while actual.siguiente is not None:
            actual = actual.siguiente
        actual.siguiente = nodo

    def buscar(self, valor):
        """True si el valor esta en la lista."""
        actual = self.cabeza
        while actual is not None:
            if actual.valor == valor:
                return True
            actual = actual.siguiente
        return False

    def eliminar(self, valor):
        """Elimina la primera aparicion del valor.

        Lanza ValueError si el valor no esta en la lista.
        """
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
        raise ValueError("El valor no esta en la lista")

    def a_lista(self):
        """Lista de Python con los valores."""
        salida = []
        actual = self.cabeza
        while actual is not None:
            salida.append(actual.valor)
            actual = actual.siguiente
        return salida
