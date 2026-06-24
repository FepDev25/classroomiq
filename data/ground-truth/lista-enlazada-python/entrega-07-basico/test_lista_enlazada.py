import unittest

from lista_enlazada import ListaEnlazada


class TestListaEnlazada(unittest.TestCase):
    def test_insertar_y_buscar(self):
        lista = ListaEnlazada()
        lista.insertar_final(1)
        self.assertTrue(lista.buscar(1))


if __name__ == "__main__":
    unittest.main()
