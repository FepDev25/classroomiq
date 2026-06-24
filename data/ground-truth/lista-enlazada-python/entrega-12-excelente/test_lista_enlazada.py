import unittest

from lista_enlazada import ListaEnlazada


class TestListaEnlazada(unittest.TestCase):
    def setUp(self):
        self.lista = ListaEnlazada()

    def test_vacia(self):
        self.assertTrue(self.lista.esta_vacia())

    def test_insertar_y_orden(self):
        self.lista.insertar_final(1)
        self.lista.insertar_final(2)
        self.lista.insertar_inicio(0)
        self.assertEqual(self.lista.a_lista(), [0, 1, 2])

    def test_buscar(self):
        self.lista.insertar_final(1)
        self.assertTrue(self.lista.buscar(1))
        self.assertFalse(self.lista.buscar(2))

    def test_eliminar_cabeza(self):
        self.lista.insertar_final(1)
        self.lista.insertar_final(2)
        self.assertTrue(self.lista.eliminar(1))
        self.assertEqual(self.lista.a_lista(), [2])

    def test_eliminar_cola(self):
        self.lista.insertar_final(1)
        self.lista.insertar_final(2)
        self.assertTrue(self.lista.eliminar(2))
        self.lista.insertar_final(3)
        self.assertEqual(self.lista.a_lista(), [1, 3])

    def test_eliminar_inexistente(self):
        self.assertFalse(self.lista.eliminar(7))


if __name__ == "__main__":
    unittest.main()
