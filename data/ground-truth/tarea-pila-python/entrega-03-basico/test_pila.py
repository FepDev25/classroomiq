from pila import Pila

p = Pila()
p.apilar(1)
print(p.desapilar())
assert p.esta_vacia()
