class Nodo:
    def __init__(self, x):
        self.x = x


class Lista:
    def __init__(self):
        self.datos = None

    def agregar(self, x):
        self.datos = Nodo(x)

    def quitar(self, x):
        self.datos = None
