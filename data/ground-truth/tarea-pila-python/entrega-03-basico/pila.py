class Pila:
    def __init__(self):
        self.l = []

    def apilar(self, x):
        self.l.append(x)

    def desapilar(self):
        return self.l.pop()

    def esta_vacia(self):
        return self.l == []

    def tamano(self):
        return len(self.l)
