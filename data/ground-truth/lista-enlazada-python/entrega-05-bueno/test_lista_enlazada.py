import unittest

from lista_enlazada import ListaEnlazada


class TestListaEnlazada(unittest.TestCase):
    def test_insertar(self):
        l = ListaEnlazada()
        l.insertar_final(1)
        l.insertar_final(2)
        self.assertEqual(l.a_lista(), [1, 2])

    def test_inicio(self):
        l = ListaEnlazada()
        l.insertar_inicio(1)
        l.insertar_inicio(2)
        self.assertEqual(l.a_lista(), [2, 1])

    def test_buscar(self):
        l = ListaEnlazada()
        l.insertar_final(1)
        self.assertTrue(l.buscar(1))
        self.assertFalse(l.buscar(2))

    def test_eliminar(self):
        l = ListaEnlazada()
        l.insertar_final(1)
        l.insertar_final(2)
        l.eliminar(1)
        self.assertEqual(l.a_lista(), [2])


if __name__ == "__main__":
    unittest.main()
