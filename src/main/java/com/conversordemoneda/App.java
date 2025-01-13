package com.conversordemoneda;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Hello world!
 *
 */
public class App
{
    private static final String API_URL = "https://v6.exchangerate-api.com/v6/c8cf2f6f3d297d1c91e8fffa/latest/USD";
    private static final Scanner scanner = new Scanner(System.in);

    // Enum para las monedas permitidas
    private enum Moneda {
        ARS("Peso argentino"),
        BOB("Boliviano boliviano"),
        BRL("Real brasileño"),
        CLP("Peso chileno"),
        COP("Peso colombiano"),
        USD("Dólar estadounidense");

        private final String descripcion;

        Moneda(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public static void main( String[] args )
    {
        boolean continuar = true;

        while (continuar) {
            try {
                mostrarMenu();
                int opcion = obtenerOpcionUsuario();

                if (opcion == 0) {
                    continuar = false;
                    System.out.println("¡Gracias por usar el conversor de monedas!");
                    continue;
                }

                // Obtener moneda origen
                System.out.println("\n┌─────────────────────────────┐");
                System.out.println("│    MONEDA DE ORIGEN         │");
                System.out.println("└─────────────────────────────┘");
                Moneda monedaOrigen = seleccionarMoneda();

                // Obtener moneda destino
                System.out.println("\n┌─────────────────────────────┐");
                System.out.println("│    MONEDA DE DESTINO        │");
                System.out.println("└─────────────────────────────┘");
                Moneda monedaDestino = seleccionarMoneda();

                // Obtener cantidad a convertir
                System.out.println("\n┌─────────────────────────────┐");
                System.out.println("│    CANTIDAD A CONVERTIR     │");
                System.out.println("└─────────────────────────────┘");
                System.out.print("$ Ingrese el monto: ");
                double cantidad = scanner.nextDouble();

                // Realizar conversión
                System.out.println("\n>>> Calculando...");
                String[] tasas = obtenerTasasDeCambio(monedaOrigen, monedaDestino);
                double tasaOrigen = Double.parseDouble(tasas[0]);
                double tasaDestino = Double.parseDouble(tasas[1]);

                // Calcular conversión a través de USD como moneda puente
                double cantidadEnUSD = cantidad / tasaOrigen;
                double resultado = cantidadEnUSD * tasaDestino;

                // Mostrar resultado
                System.out.println("\n┌─────────────────────────────┐");
                System.out.println("│         RESULTADO           │");
                System.out.println("└─────────────────────────────┘");
                System.out.printf("$ %,.2f %s \n-> %,.2f %s%n",
                    cantidad,
                    monedaOrigen.getDescripcion(),
                    resultado,
                    monedaDestino.getDescripcion());

                // Preguntar si desea continuar
                System.out.println("\n┌─────────────────────────────┐");
                System.out.println("│    ¿OTRA CONVERSIÓN?        │");
                System.out.println("└─────────────────────────────┘");
                System.out.print(">> ¿Desea realizar otra conversión? (s/n): ");
                continuar = scanner.next().toLowerCase().startsWith("s");

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                scanner.nextLine(); // Limpiar el buffer
            }
        }
        scanner.close();
    }

    private static void mostrarMenu() {
        System.out.println("\n╔════════════════════════════╗");
        System.out.println("║    CONVERSOR DE MONEDAS    ║");
        System.out.println("╠════════════════════════════╣");
        System.out.println("║  1. Realizar conversión    ║");
        System.out.println("║  0. Salir                  ║");
        System.out.println("╚════════════════════════════╝");
        System.out.print("-> Seleccione una opción: ");
    }

    private static int obtenerOpcionUsuario() {
        int opcion = scanner.nextInt();
        scanner.nextLine(); // Limpiar el buffer
        return opcion;
    }

    private static Moneda seleccionarMoneda() {
        Moneda[] monedas = Moneda.values();
        for (int i = 0; i < monedas.length; i++) {
            System.out.printf("%d. %s (%s)%n", i + 1, monedas[i].getDescripcion(), monedas[i].name());
        }
        System.out.print("Seleccione una opción: ");
        int seleccion = scanner.nextInt();
        scanner.nextLine(); // Limpiar el buffer
        return monedas[seleccion - 1];
    }

    private static String[] obtenerTasasDeCambio(Moneda moneda1, Moneda moneda2) throws Exception {
        String[] tasas = new String[2];

        // Verificar que las monedas sean válidas
        if (!esMonedaValida(moneda1) || !esMonedaValida(moneda2)) {
            throw new IllegalArgumentException("Una o ambas monedas no están en la lista permitida");
        }

        // Crear cliente HTTP
        HttpClient client = HttpClient.newHttpClient();

        // Crear solicitud HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .GET()
                .build();

        // Enviar solicitud y obtener respuesta
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Verificar estado de la respuesta
        if (response.statusCode() == 200) {
            // Parsear respuesta JSON
            Gson gson = new Gson();
            JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);

            // Extraer información de tasas de cambio específicas
            JsonObject rates = jsonResponse.getAsJsonObject("conversion_rates");
            tasas[0] = rates.get(moneda1.name()).getAsString();
            tasas[1] = rates.get(moneda2.name()).getAsString();
        } else {
            throw new RuntimeException("Error: Código de respuesta " + response.statusCode());
        }

        return tasas;
    }

    private static boolean esMonedaValida(Moneda moneda) {
        return moneda != null;
    }
}
