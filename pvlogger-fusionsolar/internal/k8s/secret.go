package k8s

import (
	"context"
	"fmt"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"

	"pvlogger-go-shared/logging"
)

type SecretReader struct {
	client    *kubernetes.Clientset
	namespace string
}

func NewSecretReader(namespace string) (*SecretReader, error) {
	config, err := rest.InClusterConfig()
	if err != nil {
		return nil, fmt.Errorf("failed to get in-cluster config: %w", err)
	}

	clientset, err := kubernetes.NewForConfig(config)
	if err != nil {
		return nil, fmt.Errorf("failed to create kubernetes client: %w", err)
	}

	return &SecretReader{
		client:    clientset,
		namespace: namespace,
	}, nil
}

func (sr *SecretReader) ReadToken(ctx context.Context, secretName, tokenKey string) (string, error) {
	logging.Info("reading token from K8s secret", "secret", secretName, "key", tokenKey)

	secret, err := sr.client.CoreV1().Secrets(sr.namespace).Get(ctx, secretName, metav1.GetOptions{})
	if err != nil {
		return "", fmt.Errorf("failed to get secret %s: %w", secretName, err)
	}

	tokenBytes, exists := secret.Data[tokenKey]
	if !exists {
		return "", fmt.Errorf("key %s not found in secret %s", tokenKey, secretName)
	}

	logging.Info("token read successfully from K8s secret")
	return string(tokenBytes), nil
}
