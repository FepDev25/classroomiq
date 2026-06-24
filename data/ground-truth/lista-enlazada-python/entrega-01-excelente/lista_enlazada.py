"""Lista enlazada simple (singly linked list) en Python puro."""


class _Nodo:
    """Nodo interno: guarda un valor y la referencia al siguiente nodo."""

    __slots__ = ("valor", "siguiente")

    def __init__(self, valor):
        self.valor = valor
        self.siguiente = None


class ListaEnlazada:
    """Lista enlazada simple.

    Mantiene referencias a la cabeza y a la cola para insertar en O(1) en ambos
    extremos, y un contador para responder ``tamano`` en O(1). Conserva la
    integridad de los enlaces en todos los casos de borde.
    """

    def __init__(self):
        self._cabeza = None
        self._cola = None
        self._tamano = 0

    def esta_vacia(self):
        """Indica si la lista no tiene elementos."""
        return self._cabeza is None

    def tamano(self):
        """Cantidad de elementos en la lista."""
        return self._tamano

    def insertar_inicio(self, valor):
        """Inserta un elemento al principio de la lista."""
        nodo = _Nodo(valor)
        nodo.siguiente = self._cabeza
        self._cabeza = nodo
        if self._cola is None:
            self._cola = nodo
        self._tamano += 1

    def insertar_final(self, valor):
        """Inserta un elemento al final de la lista."""
        nodo = _Nodo(valor)
        if self._cola is None:
            self._cabeza = self._cola = nodo
        else:
            self._cola.siguiente = nodo
            self._cola = nodo
        self._tamano += 1

    def buscar(self, valor):
        """Devuelve True si el valor está en la lista."""
        actual = self._cabeza
        while actual is not None:
            if actual.valor == valor:
                return True
            actual = actual.siguiente
        return False

    def eliminar(self, valor):
        """Elimina la primera aparición de un valor.

        Devuelve True si se eliminó, False si el valor no estaba. Actualiza la
        cabeza y la cola cuando corresponde para no dejar referencias colgantes.
        """
        anterior = None
        actual = self._cabeza
        while actual is not None:
            if actual.valor == valor:
                if anterior is None:
                    self._cabeza = actual.siguiente
                else:
                    anterior.siguiente = actual.siguiente
                if actual is self._cola:
                    self._cola = anterior
                self._tamano -= 1
                return True
            anterior = actual
            actual = actual.siguiente
        return False

    def a_lista(self):
        """Devuelve los elementos como una lista de Python, de la cabeza a la cola."""
        return list(self)

    def __iter__(self):
        actual = self._cabeza
        while actual is not None:
            yield actual.valor
            actual = actual.siguiente

    def __len__(self):
        return self._tamano
