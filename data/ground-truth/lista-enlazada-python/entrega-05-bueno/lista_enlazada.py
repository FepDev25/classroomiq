class Nodo:
    def __init__(self, v):
        self.v = v
        self.sig = None


class ListaEnlazada:
    def __init__(self):
        self.h = None

    def esta_vacia(self):
        return self.h is None

    def tamano(self):
        c = 0
        x = self.h
        while x is not None:
            c += 1
            x = x.sig
        return c

    def insertar_inicio(self, v):
        n = Nodo(v)
        n.sig = self.h
        self.h = n

    def insertar_final(self, v):
        n = Nodo(v)
        if self.h is None:
            self.h = n
            return
        x = self.h
        while x.sig is not None:
            x = x.sig
        x.sig = n

    def buscar(self, v):
        x = self.h
        while x is not None:
            if x.v == v:
                return True
            x = x.sig
        return False

    def eliminar(self, v):
        ant = None
        x = self.h
        while x is not None:
            if x.v == v:
                if ant is None:
                    self.h = x.sig
                else:
                    ant.sig = x.sig
                return
            ant = x
            x = x.sig

    def a_lista(self):
        r = []
        x = self.h
        while x is not None:
            r.append(x.v)
            x = x.sig
        return r
