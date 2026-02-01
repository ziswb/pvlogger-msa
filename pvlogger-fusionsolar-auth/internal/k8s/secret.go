package k8s

import (
	"context"
	"fmt"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"

	"pvlogger-go-shared/logging"
)

type SecretManager struct {
	client    *kubernetes.Clientset
	namespace string
}

func NewSecretManager(namespace string) (*SecretManager, error) {
	config, err := rest.InClusterConfig()
	if err != nil {
		return nil, fmt.Errorf("failed to get in-cluster config: %w", err)
	}

	clientset, err := kubernetes.NewForConfig(config)
	if err != nil {
		return nil, fmt.Errorf("failed to create kubernetes client: %w", err)
	}

	return &SecretManager{
		client:    clientset,
		namespace: namespace,
	}, nil
}

func (sm *SecretManager) UpdateToken(ctx context.Context, secretName, tokenKey, token string) error {
	secretsClient := sm.client.CoreV1().Secrets(sm.namespace)

	existing, err := secretsClient.Get(ctx, secretName, metav1.GetOptions{})
	if err != nil {
		if errors.IsNotFound(err) {
			return sm.createSecret(ctx, secretName, tokenKey, token)
		}
		return fmt.Errorf("failed to get secret: %w", err)
	}

	return sm.updateSecret(ctx, existing, tokenKey, token)
}

func (sm *SecretManager) createSecret(ctx context.Context, secretName, tokenKey, token string) error {
	logging.Info("creating new secret", "name", secretName, "namespace", sm.namespace)

	secret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{
			Name:      secretName,
			Namespace: sm.namespace,
			Labels: map[string]string{
				"app":        "pvlogger-fusionsolar-auth",
				"managed-by": "pvlogger-fusionsolar-auth",
			},
		},
		Type: corev1.SecretTypeOpaque,
		StringData: map[string]string{
			tokenKey: token,
		},
	}

	_, err := sm.client.CoreV1().Secrets(sm.namespace).Create(ctx, secret, metav1.CreateOptions{})
	if err != nil {
		return fmt.Errorf("failed to create secret: %w", err)
	}

	logging.Info("secret created successfully", "name", secretName)
	return nil
}

func (sm *SecretManager) updateSecret(ctx context.Context, secret *corev1.Secret, tokenKey, token string) error {
	logging.Info("updating existing secret", "name", secret.Name, "namespace", sm.namespace)

	if secret.Data == nil {
		secret.Data = make(map[string][]byte)
	}
	secret.Data[tokenKey] = []byte(token)

	_, err := sm.client.CoreV1().Secrets(sm.namespace).Update(ctx, secret, metav1.UpdateOptions{})
	if err != nil {
		return fmt.Errorf("failed to update secret: %w", err)
	}

	logging.Info("secret updated successfully", "name", secret.Name)
	return nil
}
