<?php

namespace App\Modules\GmTool\Foundation;

class Controller
{
    protected array $viewVars = [];

    protected function setVar(string $name, mixed $value): void
    {
        $this->viewVars[$name] = $value;
    }
}