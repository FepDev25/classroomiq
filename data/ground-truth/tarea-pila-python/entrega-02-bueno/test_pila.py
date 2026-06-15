import unittest

from pila import Pila


class TestPila(unittest.TestCase):
    def test_apilar_y_desapilar(self):
        p = Pila()
        p.apilar(1)
        p.apilar(2)
        self.assertEqual(p.desapilar(), 2)
        self.assertEqual(p.desapilar(), 1)

    def test_esta_vacia(self):
        p = Pila()
        self.assertTrue(p.esta_vacia())
        p.apilar(5)
        self.assertFalse(p.esta_vacia())

    def test_tamano(self):
        p = Pila()
        p.apilar(1)
        p.apilar(2)
        self.assertEqual(p.tamano(), 2)


if __name__ == "__main__":
    unittest.main()
