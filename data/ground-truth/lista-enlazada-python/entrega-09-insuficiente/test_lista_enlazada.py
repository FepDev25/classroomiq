import unittest

from lista import ListaEnlazada


class TestListaEnlazada(unittest.TestCase):
    def test_insertar(self):
        lista = ListaEnlazada()
        lista.insertar_final(1)
        self.assertEqual(lista.tamano(), 1)


if __name__ == "__main__":
    unittest.main()
