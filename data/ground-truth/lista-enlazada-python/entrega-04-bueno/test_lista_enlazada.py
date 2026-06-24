import unittest

from lista_enlazada import ListaEnlazada


class TestListaEnlazada(unittest.TestCase):
    def setUp(self):
        self.lista = ListaEnlazada()

    def test_insertar(self):
        self.lista.insertar_final(1)
        self.lista.insertar_final(2)
        self.assertEqual(self.lista.a_lista(), [1, 2])

    def test_insertar_inicio(self):
        self.lista.insertar_inicio(0)
        self.lista.insertar_final(1)
        self.assertEqual(self.lista.a_lista(), [0, 1])

    def test_buscar(self):
        self.lista.insertar_final(1)
        self.assertTrue(self.lista.buscar(1))
        self.assertFalse(self.lista.buscar(2))

    def test_eliminar(self):
        self.lista.insertar_final(1)
        self.lista.insertar_final(2)
        self.lista.eliminar(2)
        self.assertEqual(self.lista.a_lista(), [1])


if __name__ == "__main__":
    unittest.main()
