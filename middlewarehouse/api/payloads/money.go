package payloads

type Money struct {
	Currency string `json:"currency"`
	Value    int    `json:"value"`
}
