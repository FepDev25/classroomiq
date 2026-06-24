"""Pruebas de la lista enlazada."""

import unittest

from lista_enlazada import ListaEnlazada


class TestListaEnlazada(unittest.TestCase):
    def setUp(self):
        self.lista = ListaEnlazada()

    def test_nueva_vacia(self):
        self.assertTrue(self.lista.esta_vacia())
        self.assertEqual(self.lista.tamano(), 0)

    def test_insertar_final(self):
        self.lista.insertar_final(1)
        self.lista.insertar_final(2)
        self.assertEqual(self.lista.a_lista(), [1, 2])

    def test_insertar_inicio(self):
        self.lista.insertar_inicio(1)
        self.lista.insertar_inicio(2)
        self.assertEqual(self.lista.a_lista(), [2, 1])

    def test_buscar(self):
        self.lista.insertar_final(5)
        self.assertTrue(self.lista.buscar(5))
        self.assertFalse(self.lista.buscar(6))

    def test_eliminar_cabeza(self):
        self.lista.insertar_final(1)
        self.lista.insertar_final(2)
        self.assertTrue(self.lista.eliminar(1))
        self.assertEqual(self.lista.a_lista(), [2])

    def test_eliminar_intermedio(self):
        for v in (1, 2, 3):
            self.lista.insertar_final(v)
        self.assertTrue(self.lista.eliminar(2))
        self.assertEqual(self.lista.a_lista(), [1, 3])

    def test_eliminar_inexistente(self):
        self.lista.insertar_final(1)
        self.assertFalse(self.lista.eliminar(9))

    def test_eliminar_vacia(self):
        self.assertFalse(self.lista.eliminar(1))


if __name__ == "__main__":
    unittest.main()
