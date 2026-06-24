import unittest

from lista_enlazada import ListaEnlazada


class TestListaEnlazada(unittest.TestCase):
    def test_insertar_final(self):
        lista = ListaEnlazada()
        lista.insertar_final(1)
        lista.insertar_final(2)
        self.assertEqual(lista.a_lista(), [1, 2])

    def test_eliminar(self):
        lista = ListaEnlazada()
        lista.insertar_final(1)
        lista.insertar_final(2)
        lista.eliminar(1)
        self.assertEqual(lista.a_lista(), [2])

    def test_tamano(self):
        lista = ListaEnlazada()
        lista.insertar_final(1)
        self.assertEqual(lista.tamano(), 1)


if __name__ == "__main__":
    unittest.main()
