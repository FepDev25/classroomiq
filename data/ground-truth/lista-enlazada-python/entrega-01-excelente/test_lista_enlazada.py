"""Pruebas de ListaEnlazada, incluyendo casos de borde."""

import unittest

from lista_enlazada import ListaEnlazada


class TestListaEnlazada(unittest.TestCase):
    def setUp(self):
        self.lista = ListaEnlazada()

    def test_lista_nueva_esta_vacia(self):
        self.assertTrue(self.lista.esta_vacia())
        self.assertEqual(self.lista.tamano(), 0)

    def test_insertar_final_mantiene_orden(self):
        for v in (1, 2, 3):
            self.lista.insertar_final(v)
        self.assertEqual(self.lista.a_lista(), [1, 2, 3])

    def test_insertar_inicio_invierte_orden(self):
        for v in (1, 2, 3):
            self.lista.insertar_inicio(v)
        self.assertEqual(self.lista.a_lista(), [3, 2, 1])

    def test_buscar_encuentra_y_no_encuentra(self):
        self.lista.insertar_final("a")
        self.assertTrue(self.lista.buscar("a"))
        self.assertFalse(self.lista.buscar("z"))

    def test_eliminar_cabeza(self):
        for v in (1, 2, 3):
            self.lista.insertar_final(v)
        self.assertTrue(self.lista.eliminar(1))
        self.assertEqual(self.lista.a_lista(), [2, 3])

    def test_eliminar_cola_actualiza_referencia(self):
        for v in (1, 2, 3):
            self.lista.insertar_final(v)
        self.assertTrue(self.lista.eliminar(3))
        self.lista.insertar_final(4)
        self.assertEqual(self.lista.a_lista(), [1, 2, 4])

    def test_eliminar_inexistente_devuelve_false(self):
        self.lista.insertar_final(1)
        self.assertFalse(self.lista.eliminar(99))
        self.assertEqual(self.lista.tamano(), 1)

    def test_eliminar_en_lista_vacia_devuelve_false(self):
        self.assertFalse(self.lista.eliminar(1))


if __name__ == "__main__":
    unittest.main()
