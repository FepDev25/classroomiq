class Pila:
    def __init__(self):
        self.datos = []
    def apilar(self,x):
        self.datos.append(x)
    def desapilar(self):
        # quita el primero -> esto NO es LIFO
        return self.datos.pop(0)
