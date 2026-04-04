#!/usr/bin/env python3
"""
Simple MCP HTTP Client for testing the MCP HTTP endpoint.
Usage: python test-mcp-http.py [api_key]
"""

import requests
import json
import sys

DEFAULT_URL = "http://localhost:8080/origin/mcp"


def call_mcp(method, params=None, id=1, api_key=None):
    """Call MCP endpoint with JSON-RPC 2.0 request."""
    headers = {"Content-Type": "application/json"}
    if api_key:
        headers["X-API-Key"] = api_key

    payload = {
        "jsonrpc": "2.0",
        "method": method,
        "id": id
    }
    if params:
        payload["params"] = params

    print(f"Request: {json.dumps(payload, indent=2)}")

    try:
        response = requests.post(DEFAULT_URL, headers=headers, json=payload, timeout=10)
        if response.status_code == 200:
            result = response.json()
            print(f"Response: {json.dumps(result, indent=2)}")
            return result
        else:
            print(f"HTTP Error: {response.status_code}")
            print(f"Body: {response.text}")
            return None
    except Exception as e:
        print(f"Error: {e}")
        return None


def main():
    api_key = sys.argv[1] if len(sys.argv) > 1 else None

    print("=" * 50)
    print("MCP HTTP Client Test")
    print(f"Endpoint: {DEFAULT_URL}")
    print(f"API Key: {'<none>' if not api_key else api_key[:8] + '...'}")
    print("=" * 50)

    while True:
        print("\n--- MCP HTTP Client ---")
        print("1. List tools (tools/list)")
        print("2. Call tool")
        print("3. Send notification")
        print("4. Test error handling")
        print("0. Exit")

        choice = input("\nChoice: ").strip()

        if choice == "1":
            call_mcp("tools/list", {}, 1, api_key)
        elif choice == "2":
            method = input("Method name: ").strip()
            params_str = input("Params (JSON, e.g. {} or {\"city\":\"Beijing\"}): ").strip()
            try:
                params = json.loads(params_str) if params_str else {}
            except:
                params = {}
            call_mcp(method, params, 1, api_key)
        elif choice == "3":
            method = input("Method name (notification): ").strip()
            params_str = input("Params (JSON): ").strip()
            try:
                params = json.loads(params_str) if params_str else {}
            except:
                params = {}
            payload = {"jsonrpc": "2.0", "method": method}
            if params:
                payload["params"] = params
            print(f"Request (notification, no response expected): {json.dumps(payload, indent=2)}")
            call_mcp(method, params, None, api_key)
        elif choice == "4":
            print("\nTest invalid JSON:")
            import requests
            resp = requests.post(DEFAULT_URL, headers={"Content-Type": "application/json"}, data="invalid json")
            print(f"Response: {resp.text}")

            print("\nTest invalid request:")
            call_mcp("", {}, 1, api_key)
        elif choice == "0":
            print("Goodbye!")
            break


if __name__ == "__main__":
    main()
