import { NextRequest, NextResponse } from 'next/server';

const API_BASE_URL = () => process.env.API_BASE_URL || 'http://api:8080';

async function proxyRequest(request: NextRequest, { params }: { params: Promise<{ path: string[] }> }) {
  const { path } = await params;
  const targetPath = `/api/v1/${path.join('/')}`;
  const url = new URL(targetPath, API_BASE_URL());

  // Forward query parameters
  request.nextUrl.searchParams.forEach((value, key) => {
    url.searchParams.append(key, value);
  });

  // Forward headers, excluding host and connection-specific headers
  const headers = new Headers();
  request.headers.forEach((value, key) => {
    if (!['host', 'connection', 'keep-alive', 'transfer-encoding'].includes(key.toLowerCase())) {
      headers.set(key, value);
    }
  });

  const fetchOptions: RequestInit = {
    method: request.method,
    headers,
  };

  // Forward body for non-GET/HEAD requests
  if (request.method !== 'GET' && request.method !== 'HEAD') {
    const contentType = request.headers.get('content-type') || '';
    if (contentType.includes('multipart/form-data')) {
      fetchOptions.body = await request.arrayBuffer();
    } else {
      fetchOptions.body = await request.text();
    }
  }

  try {
    const response = await fetch(url.toString(), fetchOptions);

    // Forward response headers
    const responseHeaders = new Headers();
    response.headers.forEach((value, key) => {
      if (!['transfer-encoding', 'connection'].includes(key.toLowerCase())) {
        responseHeaders.set(key, value);
      }
    });

    const body = response.body ? await response.arrayBuffer() : null;
    return new NextResponse(body, {
      status: response.status,
      statusText: response.statusText,
      headers: responseHeaders,
    });
  } catch (error) {
    console.error(`Failed to proxy ${request.method} ${targetPath}:`, error);
    return NextResponse.json(
      { status: 502, error: 'Bad Gateway', message: 'Failed to reach backend API' },
      { status: 502 }
    );
  }
}

export const GET = proxyRequest;
export const POST = proxyRequest;
export const PUT = proxyRequest;
export const DELETE = proxyRequest;
export const PATCH = proxyRequest;
