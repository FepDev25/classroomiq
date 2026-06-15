"""Pruebas de la clase Pila, incluyendo casos de borde."""

import unittest

from pila import Pila, PilaVaciaError


class TestPila(unittest.TestCase):
    def setUp(self):
        self.pila = Pila()

    def test_pila_nueva_esta_vacia(self):
        self.assertTrue(self.pila.esta_vacia())
        self.assertEqual(self.pila.tamano(), 0)

    def test_apilar_y_desapilar_respeta_lifo(self):
        self.pila.apilar(1)
        self.pila.apilar(2)
        self.pila.apilar(3)
        self.assertEqual(self.pila.desapilar(), 3)
        self.assertEqual(self.pila.desapilar(), 2)
        self.assertEqual(self.pila.desapilar(), 1)

    def test_tope_no_quita_el_elemento(self):
        self.pila.apilar("a")
        self.assertEqual(self.pila.tope(), "a")
        self.assertEqual(self.pila.tamano(), 1)

    def test_desapilar_pila_vacia_lanza_error(self):
        with self.assertRaises(PilaVaciaError):
            self.pila.desapilar()

    def test_tope_pila_vacia_lanza_error(self):
        with self.assertRaises(PilaVaciaError):
            self.pila.tope()


if __name__ == "__main__":
    unittest.main()
