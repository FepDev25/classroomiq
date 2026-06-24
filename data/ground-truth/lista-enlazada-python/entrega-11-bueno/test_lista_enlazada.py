import unittest

from lista_enlazada import ListaEnlazada


class TestListaEnlazada(unittest.TestCase):
    def setUp(self):
        self.lista = ListaEnlazada()

    def test_insertar_final(self):
        self.lista.insertar_final(1)
        self.lista.insertar_final(2)
        self.assertEqual(self.lista.a_lista(), [1, 2])

    def test_insertar_inicio(self):
        self.lista.insertar_inicio(1)
        self.lista.insertar_inicio(2)
        self.assertEqual(self.lista.a_lista(), [2, 1])

    def test_buscar(self):
        self.lista.insertar_final(9)
        self.assertTrue(self.lista.buscar(9))
        self.assertFalse(self.lista.buscar(0))

    def test_eliminar(self):
        self.lista.insertar_final(1)
        self.lista.insertar_final(2)
        self.assertTrue(self.lista.eliminar(1))
        self.assertEqual(self.lista.a_lista(), [2])


if __name__ == "__main__":
    unittest.main()
