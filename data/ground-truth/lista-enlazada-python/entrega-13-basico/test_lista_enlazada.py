import unittest

from lista_enlazada import ListaEnlazada


class TestListaEnlazada(unittest.TestCase):
    def test_insertar_final(self):
        lista = ListaEnlazada()
        lista.insertar_final(1)
        lista.insertar_final(2)
        self.assertEqual(lista.a_lista(), [1, 2])

    def test_buscar(self):
        lista = ListaEnlazada()
        lista.insertar_final(1)
        self.assertTrue(lista.buscar(1))


if __name__ == "__main__":
    unittest.main()
